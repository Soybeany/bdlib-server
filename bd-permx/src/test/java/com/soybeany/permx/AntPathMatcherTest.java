package com.soybeany.permx;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.AntPathMatcher;

@SpringBootTest(classes = MainApplication.class)
class AntPathMatcherTest {

    @Test
    public void test() {
        AntPathMatcher matcher = new AntPathMatcher();
        boolean match = matcher.match("/sdf/**", "/sdf/wh/sdf");
        System.out.println(match);
    }

}