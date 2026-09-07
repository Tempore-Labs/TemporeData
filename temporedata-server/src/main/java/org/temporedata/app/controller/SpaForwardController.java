package org.temporedata.app.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * SPA fallback: for browser (HTML) navigation to a client-side route or an
 * unmatched path, forward to index.html so vue-router can render the page.
 * API (/api) 404s are returned as JSON instead.
 */
@Controller
public class SpaForwardController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        // Only translate browser HTML navigations to the SPA entry.
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.OK.value());
            return "forward:/index.html";
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"code\":404,\"msg\":\"资源不存在\",\"data\":null}");
    }
}