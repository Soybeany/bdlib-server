package com.soybeany.cache.v2.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 操作结果
 * <br>Created by Soybeany on 2020/10/15.
 */
public class ActionResult {

    private final List<String> successList;
    private final List<String> failList;

    private ActionResult(List<String> successList, List<String> failList) {
        this.successList = successList;
        this.failList = failList;
    }

    public boolean isAllSuccess() {
        return !successList.isEmpty() && failList.isEmpty();
    }

    public List<String> getSuccessList() {
        return successList;
    }

    public List<String> getFailList() {
        return failList;
    }

    public static class Builder {
        private final List<String> successList = new LinkedList<String>();
        private final List<String> failList = new LinkedList<String>();

        public void addSuccessItem(String item) {
            successList.add(item);
        }

        public void addFailItem(String item) {
            failList.add(item);
        }

        public ActionResult build() {
            return new ActionResult(Collections.unmodifiableList(successList), Collections.unmodifiableList(failList));
        }
    }
}
