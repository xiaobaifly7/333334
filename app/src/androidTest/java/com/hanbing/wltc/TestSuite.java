package com.hanbing.wltc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 在线弹窗系统测试套件
 * 整合所有测试用例，一次性运行所有测试
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        FullPopupSystemTest.class,
        OnlineDialogLoadingTest.class
})
public class TestSuite {
    // 测试套件不需要实现任何代码
} 