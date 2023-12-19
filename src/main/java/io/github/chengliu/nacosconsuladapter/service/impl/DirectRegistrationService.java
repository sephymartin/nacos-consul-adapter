/**
 * The MIT License Copyright © 2021 liu cheng
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.chengliu.nacosconsuladapter.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

import io.github.chengliu.nacosconsuladapter.model.Result;
import io.github.chengliu.nacosconsuladapter.model.ServiceInstancesHealth;
import io.github.chengliu.nacosconsuladapter.model.ServiceInstancesHealthOld;
import io.github.chengliu.nacosconsuladapter.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @description:直接请求注册中心模式
 * @author: lc
 * @createDate: 2021/5/31
 */
@RequiredArgsConstructor
@Slf4j
public class DirectRegistrationService implements RegistrationService {

    private final ReactiveDiscoveryClient reactiveDiscoveryClient;

    @Override
    public Mono<Result<Map<String, List<Object>>>> getServiceNames(long waitMillis, Long index) {
        return reactiveDiscoveryClient.getServices().collectList().switchIfEmpty(Mono.just(Collections.emptyList()))
            .map(serviceList -> {
                Set<String> set = new HashSet<>();
                set.addAll(serviceList);
                Map<String, List<Object>> result = new HashMap<>(serviceList.size());
                for (String item : set) {
                    List<ServiceInstance> serviceInstances =
                        reactiveDiscoveryClient.getInstances(item).collectList().block();
                    List<Object> list = new ArrayList<>();
                    for (ServiceInstance serviceInstance : serviceInstances) {
                        Map<String, String> metadata = serviceInstance.getMetadata();
                        if (metadata.containsKey(META_MGMT_PORT)) {
                            list.add(serviceInstance.getHost() + ":" + metadata.get(META_MGMT_PORT));
                        }else {
                            log.warn("{} 不包含 {} 端口管理信息", serviceInstance, META_MGMT_PORT);
                        }
                    }
                    if (!list.isEmpty()) {
                        result.put(item, list);
                    }
                }
                return result;
            }).map(data -> new Result<>(data, System.currentTimeMillis()));
    }

    @Override
    public Mono<Result<List<ServiceInstancesHealth>>> getServiceInstancesHealth(String serviceName, long waitMillis,
        Long index) {
        return reactiveDiscoveryClient.getInstances(serviceName).map(serviceInstance -> {

            Map<String, String> metadataMap = serviceInstance.getMetadata();
            metadataMap.put(NACOS_APPLICATION_NAME, serviceName);

            ServiceInstancesHealth.Node node = ServiceInstancesHealth.Node.builder().address(serviceInstance.getHost())
                .id(serviceInstance.getInstanceId())
                // todo 数据中心
                .dataCenter("dc1").build();

            ServiceInstancesHealth.Service service =
                ServiceInstancesHealth.Service.builder().service(serviceInstance.getServiceId())
                    .id(serviceInstance.getServiceId() + "-" + serviceInstance.getPort())
                    .port(serviceInstance.getPort()).meta(metadataMap).build();
            return ServiceInstancesHealth.builder().node(node).service(service).build();
        }).filter(serviceInstancesHealth -> serviceInstancesHealth.getService().getMeta().containsKey(META_MGMT_PORT))
            .collectList().map(data -> new Result<>(data, System.currentTimeMillis()));
    }

    @Override
    public Mono<Result<List<ServiceInstancesHealthOld>>> getServiceInstancesHealthOld(String serviceName,
        long waitMillis, Long index) {
        return reactiveDiscoveryClient.getInstances(serviceName).map(serviceInstance -> {

            Map<String, String> metadataMap = serviceInstance.getMetadata();
            metadataMap.put(NACOS_APPLICATION_NAME, serviceName);

            ServiceInstancesHealth.Node node = ServiceInstancesHealth.Node.builder().address(serviceInstance.getHost())
                .id(serviceInstance.getInstanceId())
                // todo 数据中心
                .dataCenter("dc1").build();
            ServiceInstancesHealth.Service service =
                ServiceInstancesHealth.Service.builder().service(serviceInstance.getServiceId())
                    .id(serviceInstance.getServiceId() + "-" + serviceInstance.getPort())
                    .port(serviceInstance.getPort()).build();
            return ServiceInstancesHealth.builder().node(node).service(service).build();
        }).filter(serviceInstancesHealth -> serviceInstancesHealth.getService().getMeta().containsKey(META_MGMT_PORT))
            .map(serviceInstancesHealth -> new ServiceInstancesHealthOld(serviceInstancesHealth)).collectList()
            .map(data -> new Result<>(data, System.currentTimeMillis()));
    }

}
