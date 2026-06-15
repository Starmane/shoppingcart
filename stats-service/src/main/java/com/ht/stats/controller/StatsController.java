package com.ht.stats.controller;

import com.ht.common.enums.ActionType;
import com.ht.stats.dto.StatsResponseDto;
import com.ht.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Get the number of offers of a particular id and action recorded in a period")
    @GetMapping
    public ResponseEntity<StatsResponseDto> getStats(
            @RequestParam String offerId,
            @RequestParam ActionType action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        long count = statsService.countByOfferAndAction(offerId, action, from, to);
        return ResponseEntity.ok(new StatsResponseDto(offerId, action, from, to, count));
    }
}
