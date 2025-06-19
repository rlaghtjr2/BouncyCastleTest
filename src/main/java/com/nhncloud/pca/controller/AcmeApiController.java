package com.nhncloud.pca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.model.acme.Directory;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/acme")
public class AcmeApiController {
    @RequestMapping(value = "/directory", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Directory> getDirectory(HttpServletRequest request) {
        return ResponseEntity.ok(new Directory(request.getServerName(), request.getServerPort()));
    }
}
