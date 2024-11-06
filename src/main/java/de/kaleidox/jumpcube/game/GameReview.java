package de.kaleidox.jumpcube.game;

import com.ampznetwork.libmod.api.entity.DbObject;
import com.ampznetwork.libmod.api.entity.Player;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "jumpcube_log")
public class GameReview extends DbObject {
    private @ManyToMany List<Player> players;
    private @ManyToOne  Player       winner;
    private             int          timeSeconds;
    private             int          minedHelpers;
    private             int          placedHelpers;
}
