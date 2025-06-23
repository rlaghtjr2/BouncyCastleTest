package com.nhncloud.pca.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/acme")
public class AcmeApiController {
//    @RequestMapping(value = "/directory", method = {RequestMethod.GET, RequestMethod.POST})
//    public ResponseEntity<Directory> getDirectory(HttpServletRequest request) {
//        return ResponseEntity.ok(new Directory(request.getServerName(), request.getServerPort()));
//    }
}
