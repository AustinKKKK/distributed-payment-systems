package com.example.tier4.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(0)
public class RoutingAspect {

    @Pointcut("@annotation(transactional)")
    public void transactionalMethod(Transactional transactional) {}

    @Before("transactionalMethod(transactional)")
    public void routeDataSource(Transactional transactional) {

//        DataSourceContextHolder.set(DataSourceType.PRIMARY);
//        System.out.println("→ Routing to PRIMARY (forced, replica disabled for test)");

        if (transactional.readOnly()) {
            DataSourceContextHolder.set(DataSourceType.REPLICA);
            System.out.println("-> Routing to REPLICA");
        }else {
            DataSourceContextHolder.set(DataSourceType.PRIMARY);
            System.out.println("-> Routing to PRIMARY");
        }
    }

}
