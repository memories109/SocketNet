package com.realtime.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindValue {

	// 시작위치
	int StartPosition() default 0;
	// 길이
	int Length() default 0;
	// 필드명
	String Name();
}

