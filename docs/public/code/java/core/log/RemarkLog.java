package com.sjbb.core.log;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Retention(RUNTIME)
@Target(METHOD)
public @interface RemarkLog {
	
	@AliasFor("value")
	String description() default "";
	
	@AliasFor("description")
	String value() default "";
}
