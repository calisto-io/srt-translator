package de.calisto.srttranslator.controller;

import de.calisto.srttranslator.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranslationController {
  private final TranslationService translationService;

  @Autowired
  public TranslationController(TranslationService translationService) {
    this.translationService = translationService;
  }

  @PostMapping("/translate")
  public void translate() {
    translationService.translate();
  }
}
