package org.grails.datastore.gorm.validation.javax.services

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.javax.JavaxValidatorRegistry
import org.grails.datastore.mapping.services.Service

import javax.validation.Configuration
import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.validation.ParameterNameProvider
import javax.validation.Validation
import javax.validation.ValidatorFactory
import javax.validation.executable.ExecutableValidator
import java.lang.reflect.Method

/**
 * A service that is validated by javax.validation
 *
 * @author Graeme Rocher
 */
@CompileStatic
trait ValidatedService<T> extends Service<T> {

    /**
     * The parameter name provided for this service
     */
    ParameterNameProvider parameterNameProvider

    /**
     * The validator factory
     */
    private ValidatorFactory validatorFactory

    private Map<Method, ExecutableValidator> executableValidatorMap = new LinkedHashMap<Method,ExecutableValidator>().withDefault {
        getValidatorFactory().getValidator().forExecutables()
    }

    /**
     * @return The validator factory for this service
     */
    ValidatorFactory getValidatorFactory() {
        if(validatorFactory == null) {

            Configuration configuration
            if(datastore != null) {
                configuration = JavaxValidatorRegistry.buildConfigurationFor(
                        datastore.mappingContext,
                        datastore.mappingContext.validatorRegistry.messageSource
                )
            }
            else {
                configuration = Validation.byDefaultProvider()
                                            .configure()
                configuration = configuration.ignoreXmlConfiguration()
            }
            if(parameterNameProvider != null) {
                configuration = configuration.parameterNameProvider(parameterNameProvider)
            }
            validatorFactory = configuration.buildValidatorFactory()
        }
        return validatorFactory
    }

    /**
     * Validate the given method for the given arguments
     *
     * @param instance The instance
     * @param method The method
     * @param args The arguments
     */
    void validate(Object instance, Method method, Object...args) {
        ExecutableValidator validator = executableValidatorMap.get(method)
        Set<ConstraintViolation> constraintViolations = validator.validateParameters(instance, method, args)
        if(!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations)
        }
    }
}