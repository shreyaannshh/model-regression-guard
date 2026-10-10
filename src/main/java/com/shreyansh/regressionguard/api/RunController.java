package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.domain.Run;
import com.shreyansh.regressionguard.run.RunService;
import com.shreyansh.regressionguard.store.NotFoundException;
import com.shreyansh.regressionguard.store.Store;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runs")
public class RunController {

    private final RunService runService;
    private final Store store;

    public RunController(RunService runService, Store store) {
        this.runService = runService;
        this.store = store;
    }

    /** Replays every case against the active baseline set. Synchronous: known debt for large case counts. */
    @PostMapping
    public ResponseEntity<RunResponse> run() {
        Run run = runService.run();
        return ResponseEntity
                .created(URI.create("/runs/" + run.id()))
                .body(RunResponse.from(run));
    }

    @GetMapping
    public List<RunResponse> list() {
        return store.allRuns().stream().map(RunResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RunResponse get(@PathVariable String id) {
        return store.findRun(id)
                .map(RunResponse::from)
                .orElseThrow(() -> new NotFoundException("run", id));
    }
}