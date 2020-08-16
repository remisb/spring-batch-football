package lt.bauzys.sbatch.footbal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SbatchFootballApplication {

    public static void main(String[] args) {
        String[] newArgs = new String[] {
                "playerFile=/input/player.csv",
                "gameFile=/input/games.csv",
                "commit.interval=3"};
        SpringApplication.run(SbatchFootballApplication.class, newArgs);
    }
}
