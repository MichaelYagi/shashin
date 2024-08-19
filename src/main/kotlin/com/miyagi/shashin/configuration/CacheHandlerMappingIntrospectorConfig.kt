package com.miyagi.shashin.configuration

//import jakarta.servlet.DispatcherType
//import jakarta.servlet.Filter
//import org.springframework.boot.web.servlet.FilterRegistrationBean
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.core.Ordered
//import org.springframework.web.servlet.handler.HandlerMappingIntrospector
//import java.util.*
//
//
///**
// * @author Rob Winch
// */
//@Configuration
//class CacheHandlerMappingIntrospectorConfig {
//    @Bean
//    fun handlerMappingIntrospectorCacheFilter(hmi: HandlerMappingIntrospector): FilterRegistrationBean<Filter> {
//        val cacheFilter = hmi.createCacheFilter()
//        val registrationBean = FilterRegistrationBean(cacheFilter)
//        registrationBean.order = Ordered.HIGHEST_PRECEDENCE
//        registrationBean.setDispatcherTypes(EnumSet.allOf(DispatcherType::class.java))
//        return registrationBean
//    }
//}