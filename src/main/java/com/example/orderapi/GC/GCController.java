package com.example.orderapi.GC;


import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/gc")
public class GCController {
    // static이 참조를 계속 들고 있음 -> GC 대상이 아님
    private static final List<byte[]> LEAK = new ArrayList<>();

    // 요청 안에서 생성 후 버려지는 객체 -> Minor GC
    @GetMapping("/minor")
    public String minor(@RequestParam(defaultValue = "50")int mb){
        System.out.println("da");
        for(int i=0;i<mb*10;i++){
            byte[] tmp = new byte[100*1024];
        }
        return "dropped"+mb+"MB";
    }

    @GetMapping("/major")
    public String major(@RequestParam(defaultValue = "20")int mb){
        for(int i=0;i<mb*10;i++){
            LEAK.add(new byte[100*1024]);
        }
        return "retained"+LEAK.size()/10+"MB";
    }

    @PostMapping("clear")
    public String clear(){
        LEAK.clear(); return "cleared";
    }
}
