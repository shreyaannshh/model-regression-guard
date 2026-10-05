package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.baseline.BaselineService;
import com.shreyansh.regressionguard.domain.BaselineSet;
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
@RequestMapping("/baselines")
public class BaselineController {

    private final BaselineService baselineService;
    private final Store store;

    public BaselineController(BaselineService baselineService, Store store) {
        this.baselineService = baselineService;
        this.store = store;
    }

    /** Captures a new set. It is NOT active until POST /baselines/{id}/activate. */
    @PostMapping
    public ResponseEntity<BaselineSetResponse> capture() {
        BaselineSet set = baselineService.capture();
        return ResponseEntity
                .created(URI.create("/baselines/" + set.id()))
                .body(BaselineSetResponse.from(set, false));
    }

    @PostMapping("/{id}/activate")
    public BaselineSetResponse activate(@PathVariable String id) {
        return BaselineSetResponse.from(baselineService.activate(id), true);
    }

    @GetMapping
    public List<BaselineSetResponse> list() {
        String activeId = activeId();
        return store.allBaselineSets().stream()
                .map(set -> BaselineSetResponse.from(set, set.id().equals(activeId)))
                .toList();
    }

    @GetMapping("/{id}")
    public BaselineSetResponse get(@PathVariable String id) {
        BaselineSet set = store.findBaselineSet(id).orElseThrow(() -> new NotFoundException("baseline set", id));
        return BaselineSetResponse.from(set, set.id().equals(activeId()));
    }

    private String activeId() {
        return store.activeBaselineSet().map(BaselineSet::id).orElse(null);
    }
}