package com.shreyansh.regressionguard.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.store.Store;

@RestController 
@RequestMapping ("/cases")
public class CaseController {
    private final Store store;

    public CaseController(Store store) {
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<CaseResponse> create(@RequestBody CreateCaseRequest request) {
        GoldenCase goldenCase = request.toDomain(Instant.now());
        store.saveCase(goldenCase);
        return ResponseEntity.created(URI.create("/cases/" + goldenCase.id())).body(CaseResponse.from(goldenCase));
    }

    @GetMapping 
    public List<CaseResponse> list() {
        return store.allCases().stream().map(CaseResponse::from).toList();
    }

    @GetMapping("/{id}")
        public ResponseEntity<CaseResponse> get(@PathVariable String id) {
            return store.findCase(id)
                    .map(CaseResponse::from)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
}
