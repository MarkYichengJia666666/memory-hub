package com.yqg.core.service.apichannel.collision.config;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.platform.process_manage.annotation.ProcessInitializerConfig;
import com.yqg.platform.process_manage.config.GenericProcessInitializer;
import com.yqg.platform.process_manage.config.ProcessInitializerProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 撞库流程初始化配置。
 * 自动遍历 {@link CollisionProcessEnum} 全部枚举值注册流程定义，
 * 新增流程只需在枚举中加值，无需修改此配置。
 */
@Component
@ProcessInitializerConfig
public class CollisionProcessInitConfig implements ProcessInitializerProvider {

    /**
     * 遍历 {@link CollisionProcessEnum} 所有枚举值，为每个流程生成对应的 initializer。
     * folderPath 由枚举自身的 code 推导，nodeFactory 统一使用 {@link CollisionProcessNode#fromCode}。
     */
    @Override
    public List<GenericProcessInitializer> getInitializers() {
        return Arrays.stream(CollisionProcessEnum.values())
                .map(process -> GenericProcessInitializer.builder()
                        .folderPath(process.getFolderPath())
                        .nodeFactory(CollisionProcessNode::fromCode)
                        .process(process)
                        .build())
                .collect(Collectors.toList());
    }
}
