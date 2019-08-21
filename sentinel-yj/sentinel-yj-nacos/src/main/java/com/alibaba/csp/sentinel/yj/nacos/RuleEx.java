package com.alibaba.csp.sentinel.yj.nacos;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;

public class RuleEx<T extends AbstractRule> {
    private T rule;

    public T getRule() {
        return rule;
    }

    public void setRule(T rule) {
        this.rule = rule;
    }
}
