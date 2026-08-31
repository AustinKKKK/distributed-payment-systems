package com.example.tier3.account;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable Long id) {
        Long balance = accountService.getBalance(id);
        return new BalanceResponse(id, balance);
    }

    public record BalanceResponse(
            Long accountId,
            Long balance
    ) {}

}