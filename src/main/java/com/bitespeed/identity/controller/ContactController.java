package com.bitespeed.identity.controller;


import com.bitespeed.identity.dto.ContactRequest;
import com.bitespeed.identity.dto.ContactResponse;
import com.bitespeed.identity.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identify")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> identifyContact(@Valid @RequestBody ContactRequest request) {
        ContactResponse response = contactService.identifyContact(request);
        return ResponseEntity.ok(response);
    }
}
