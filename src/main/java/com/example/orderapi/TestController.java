package com.example.orderapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {
    @GetMapping("/temp")
    public ResponseEntity<TestDTO> temp(){
        /*Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        map.put("a", "b");
        map.put("c", "d");
        map.put("e", "f");*/

        TestDTO testDTO = new TestDTO(1L, "영수", 28);

        return ResponseEntity.ok(testDTO);
    }

    @PostMapping("/post")
    public ResponseEntity<TestDTO>post(@RequestParam(name="id") Long id,
                                       @RequestParam(name="name") String name,
                                       @RequestParam(name="age") int age){
        TestDTO testDTO = new TestDTO(id, name, age);
        return ResponseEntity.ok(testDTO);
    }

    @PostMapping("/postb")
    public ResponseEntity<TestDTO>postB(@RequestBody TestDTO testDTO){

        return ResponseEntity.ok(testDTO);
    }
}
