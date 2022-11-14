package com.controller;

import com.dto.Queued;
import com.listeners.StateMachineDebugger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MyController {

    private final StateMachineDebugger debugger;

    public MyController(StateMachineDebugger debugger) {
        this.debugger = debugger;
    }


    @RequestMapping(value = "/test")
    public List<Queued> get() {
        return debugger.getLast();
    }
}
