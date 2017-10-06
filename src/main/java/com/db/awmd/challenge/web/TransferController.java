package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.FundTransferException;
import com.db.awmd.challenge.exception.InSufficientFundException;
import com.db.awmd.challenge.exception.InValidTransferRequestException;
import com.db.awmd.challenge.service.FundTransferService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

/**
 * Rest API for Fund transfer between accounts
 */
@RestController
@RequestMapping("/v1/transfers")
@Slf4j
public class TransferController {

    private FundTransferService fundTransferService;

    @Autowired
    public TransferController(final FundTransferService fundTransferService) {
        this.fundTransferService = fundTransferService;
    }

    /**
     * Transfer's funds between two existing accounts.
     * @param transfer
     * @return ResponseEntity
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> transferFund(@RequestBody @Valid Transfer transfer) {
        log.info("Received transfer request : {}", transfer);
        fundTransferService.transferFund(transfer);
        log.info("Sucessfully processed transfer request : {}", transfer);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Exception handler for bad requests
     * @param request
     * @param exception
     * @return ResponseEntity
     */
    @ExceptionHandler({InSufficientFundException.class, AccountNotFoundException.class,
            InValidTransferRequestException.class})
    public ResponseEntity<?> handleClientSideExceptions(HttpServletRequest request, Exception exception) {
        log.error("Cancelling transfer request. Reason : {}", exception.getMessage());
        return new ResponseEntity(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler for server side exceptions
     * @param request
     * @param exception
     * @return ResponseEntity
     */
    @ExceptionHandler(FundTransferException.class)
    public ResponseEntity<?> handleFundTransferxceptions(HttpServletRequest request, FundTransferException exception) {
        log.error("Failed to process transfer request. Reason : {}", exception.getMessage());
        return new ResponseEntity(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
