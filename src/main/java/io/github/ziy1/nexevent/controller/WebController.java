package io.github.ziy1.nexevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
  @GetMapping(value = "/{path:[^\\.]*}")
  public String redirect() {
    // Redirect all non-file requests to index.html
    return "forward:/index.html";
  }
}
