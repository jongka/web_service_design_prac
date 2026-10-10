package kr.ac.jbnu.bjh.web_service_design_hw1.api.v3;

import kr.ac.jbnu.bjh.web_service_design_hw1.api.dto.ItemDto;
import kr.ac.jbnu.bjh.web_service_design_hw1.api.request.ItemCreateRequest;
import kr.ac.jbnu.bjh.web_service_design_hw1.api.request.ItemUpdateRequest;
import kr.ac.jbnu.bjh.web_service_design_hw1.api.request.ItemPriceUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v3/items")
public class ItemController3 {
    // 메모리에 저장하므로 서버 재시작 시 데이터가 초기화됩니다.
    private final Map<Long, ItemDto> store = new LinkedHashMap<>();
    private long sequence = 1L;

    // GET 1: 전체 조회
    @GetMapping
    public synchronized List<ItemDto> getItems() {
        return new ArrayList<>(store.values());
    }

    // GET 2: 단건 조회
    @GetMapping("/{id}")
    public synchronized ResponseEntity<ItemDto> getItem(@PathVariable Long id) {
        ItemDto item = store.get(id);
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        return ResponseEntity.ok(item);
    }

    // POST 1: 단건 생성
    @PostMapping
    public synchronized ResponseEntity<ItemDto> createItem(@RequestBody ItemCreateRequest request) {
        if (!isValidCreate(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
        }
        ItemDto item = saveItem(request);
        return ResponseEntity.created(URI.create("/api/v3/items/" + item.getId())).body(item);
    }

    // POST 2: 일괄 생성 (요청 본문: 상품 배열)
    @PostMapping("/bulk")
    public synchronized ResponseEntity<List<ItemDto>> createItems(@RequestBody List<ItemCreateRequest> requests) {
        if (requests == null || requests.isEmpty() || requests.stream().anyMatch(request -> !isValidCreate(request))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
        }
        List<ItemDto> created = new ArrayList<>();
        for (ItemCreateRequest request : requests) {
            created.add(saveItem(request));
        }
        return ResponseEntity.created(URI.create("/api/v3/items")).body(created);
    }

    // PUT 1: 단건 수정
    @PutMapping("/{id}")
    public synchronized ResponseEntity<ItemDto> updateItem(@PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        ItemDto item = store.get(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        if (request == null || (request.getName() == null && request.getPrice() == null)
                || (request.getName() != null && request.getName().isBlank())
                || (request.getPrice() != null && request.getPrice() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
        }
        if (request.getName() != null) item.setName(request.getName());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        return ResponseEntity.ok(item);
    }

    // PUT 2: 일괄 가격 수정 (요청 본문: id, price 배열)
    @PutMapping("/bulk/prices")
    public synchronized ResponseEntity<List<ItemDto>> updatePrices(@RequestBody List<ItemPriceUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
        Set<Long> ids = new HashSet<>();
        for (ItemPriceUpdateRequest request : requests) {
            if (request == null || request.getId() == null || request.getPrice() == null
                    || request.getPrice() < 0 || !ids.add(request.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
            }
        }
        // 모든 ID를 확인한 뒤 수정하여 일부 상품만 바뀌는 것을 방지
        if (ids.stream().anyMatch(id -> !store.containsKey(id))) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        List<ItemDto> updated = new ArrayList<>();
        for (ItemPriceUpdateRequest request : requests) {
            ItemDto item = store.get(request.getId());
            item.setPrice(request.getPrice());
            updated.add(item);
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE 1: 단건 삭제 (204, 응답 본문 없음)
    @DeleteMapping("/{id}")
    public synchronized ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        ItemDto removed = store.remove(id);
        if (removed == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        return ResponseEntity.noContent().build();
    }

    // DELETE 2: 선택한 상품 일괄 삭제 (예: /bulk?ids=1,2)
    @DeleteMapping("/bulk")
    public synchronized ResponseEntity<Void> deleteItems(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다. 이름, 가격, ID 목록을 확인하세요.");
        }
        if (ids.stream().anyMatch(id -> !store.containsKey(id))) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        List<ItemDto> removed = new ArrayList<>();
        for (Long id : ids) removed.add(store.remove(id));
        return ResponseEntity.noContent().build();
    }

    private boolean isValidCreate(ItemCreateRequest request) {
        return request != null && request.getName() != null && !request.getName().isBlank()
                && request.getPrice() != null && request.getPrice() >= 0;
    }

    private ItemDto saveItem(ItemCreateRequest request) {
        ItemDto item = new ItemDto();
        item.setId(sequence++);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        store.put(item.getId(), item);
        return item;
    }
}