package io.intellij.devops.server.dnsapi.controller;

import io.intellij.devops.server.dnsapi.config.properties.DnsApiProperties;
import io.intellij.devops.server.dnsapi.entities.Result;
import io.intellij.devops.server.dnsapi.entities.ddns.DDnsRequest;
import io.intellij.devops.server.dnsapi.entities.ddns.DDnsResponse;
import io.intellij.devops.server.dnsapi.services.DDnsService;
import io.intellij.devops.server.dnsapi.utils.DomainNameUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.intellij.devops.server.dnsapi.utils.HttpServletRequestUtils.getRequestIp;
import static io.intellij.devops.server.dnsapi.utils.HttpServletRequestUtils.isValidIPv4;

/**
 * DDnsController
 *
 * @author tech@intellij.io
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@RequestMapping("/ddns")
@Slf4j
public class DDnsController {
    private final DDnsService dDnsService;
    private final DnsApiProperties dnsApiProperties;

    @PostMapping("/invoke")
    public Result<DDnsResponse> invoke(@RequestBody DDnsRequest request) {
        validateDomain(request.getDomainName(), request.getRr());
        validateIpv4(request.getIpv4());

        log.info("ddns|domainName={}|rr={}|ipv4={}", request.getDomainName(), request.getRr(), request.getIpv4());
        return Result.ok(dDnsService.ddns(request.getDomainName(), request.getRr(), request.getIpv4()));
    }

    @PostMapping(value = {"/invokeGetIpAutomatic", "/invokeGetIpByHttpServletRequest"})
    public Result<DDnsResponse> invokeGetIpByHttpServletRequest(@RequestBody DDnsRequest request, HttpServletRequest httpServletRequest) {
        return invoke(DDnsRequest.builder().domainName(request.getDomainName()).rr(request.getRr()).ipv4(getRequestIp(httpServletRequest)).build());
    }

    private void validateDomain(String domainName, String rr) {
        Set<String> domainAclSet = new HashSet<>(dnsApiProperties.getDomainAcl());
        if (!domainAclSet.contains(domainName)) {
            throw new RuntimeException("domain access control list does not contains domainName|domainName=" + domainName);
        }

        List<String> excludeRrList = dnsApiProperties.getExcludeRrList();
        Set<String> excludeRrSet = new HashSet<>(excludeRrList);

        if (excludeRrSet.contains(rr)) {
            throw new RuntimeException("request's rr has in excluded rr list");
        }

        if (!DomainNameUtils.validateRR(rr)) {
            throw new RuntimeException("The valid pattern should include alphanumeric characters and has a length between 1 and 32.");
        }
    }

    private void validateIpv4(String ipv4) {
        if (!isValidIPv4(ipv4)) {
            throw new RuntimeException("invalid ipv4|ip=" + ipv4);
        }
    }

}
