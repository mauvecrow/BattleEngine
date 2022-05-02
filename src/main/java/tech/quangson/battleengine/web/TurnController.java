package tech.quangson.battleengine.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.quangson.battleengine.mechanics.TurnService;
import tech.quangson.battleengine.mechanics.TurnState;

@RestController
@RequestMapping("/engine")
public class TurnController {

    private final TurnService ts;

    public TurnController(TurnService ts) {
        this.ts = ts;
    }

    @PostMapping("/turn")
    public ResponseEntity<?> processTurn(@RequestBody TurnState turn){
        try {
            var resultState = ts.evaluateTurn(turn);
            return new ResponseEntity<>(resultState, HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.BAD_REQUEST);
        }
    }
}
