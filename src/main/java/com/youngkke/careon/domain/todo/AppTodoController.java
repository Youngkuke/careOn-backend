package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.todo.dto.TodoCheckRequest;
import com.youngkke.careon.domain.todo.dto.TodoCheckResponse;
import com.youngkke.careon.domain.todo.dto.TodoListResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/todos")
@RequiredArgsConstructor
public class AppTodoController {

    private final TodoService todoService;

    /** 투두 목록 조회. */
    @GetMapping
    public ResponseEntity<List<TodoListResponse>> getList(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(todoService.getList(carerId));
    }

    /** 투두 체크/체크 해제. */
    @PatchMapping("/{todoId}")
    public ResponseEntity<TodoCheckResponse> updateChecked(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer todoId,
            @Valid @RequestBody TodoCheckRequest request) {
        return ResponseEntity.ok(todoService.updateChecked(carerId, todoId, request));
    }
}
