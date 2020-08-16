package lt.bauzys.sbatch.footbal;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Value("${commit.interval:10}")
    private int commitInterval;
    @Value("${batch.verify.cursor.position:true}")
    private boolean verifyCursorPosition;

    public BatchConfiguration(JobBuilderFactory jobBuilderFactory,
                              StepBuilderFactory stepBuilderFactory,
                              DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public Job footballJob(@Qualifier("playerLoadStep") Step playerLoadStep,
                           Step gameLoadStep,
                           @Qualifier("summarizationStep") Step summarizationStep,
                           JobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("footballJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(playerLoadStep)
                .next(gameLoadStep)
                .next(summarizationStep)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Player> playerItemReader(
            @Value("#{jobParameters['playerFile']}") Resource resource) {
        return new FlatFileItemReaderBuilder<Player>()
                .name("playerItemReader")
                .resource(resource)
                .linesToSkip(0)
                .delimited()
                .names(new String[]{"ID", "lastName", "firstName", "position", "birthYear", "debutYear"})
                .fieldSetMapper(fieldSet -> {
                    Player p = new Player();
                    p.setId(fieldSet.readString("ID"));
                    p.setLastName(fieldSet.readString("lastName"));
                    p.setFirstName(fieldSet.readString("firstName"));
                    p.setPosition(fieldSet.readString("position"));
                    p.setBirthYear(fieldSet.readInt("birthYear"));
                    p.setDebutYear(fieldSet.readInt("debutYear"));
                    return p;
                })
                .build();
    }

    @Bean
    public JdbcCursorItemReader<PlayerSummary> playerSummaryJdbcReader(
            DataSource ds,
            PlayerSummaryRowMapper mapper) {

        return new JdbcCursorItemReaderBuilder<PlayerSummary>()
                .dataSource(ds)
                .name("playerSummaryJdbcReader")
//                .saveState(true)
                .rowMapper(mapper)
                .verifyCursorPosition(this.verifyCursorPosition)
                .sql("SELECT GAME.player_id, GAME.year, SUM(COMPLETES)," +
                        "SUM(ATTEMPTS), SUM(PASSING_YARDS), SUM(PASSING_TD)," +
                        "SUM(INTERCEPTIONS), SUM(RUSHES), SUM(RUSH_YARDS)," +
                        "SUM(RECEPTIONS), SUM(RECEPTION_YARDS), SUM(TOTAL_TD)" +
                        "from GAME, PLAYER where PLAYER.player_id =" +
                        "GAME.player_id group by GAME.player_id, GAME.year")
                .build();
    }

    @Bean
    public PlayerSummaryRowMapper playerSummaryRowMapper() {
        return new PlayerSummaryRowMapper();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Game> gameFlatFileItemReader(
            @Value("#{jobParameters['gameFile']}") Resource resource) {
        return new FlatFileItemReaderBuilder<Game>()
                .name("gameFileItemReader")
                .resource(resource)
                .linesToSkip(0)
                .delimited()
                .names(new String[]{"ID", "year", "team", "week", "opponent", "completes",
                        "attempts", "passingYards", "passingTd", "interceptions",
                        "rushes", "rushYards", "receptions", "receptionYards", "totalTd"})
                .fieldSetMapper(fieldSet -> {
                    Game g = new Game();
                    g.setId(fieldSet.readString("ID"));
                    g.setYear(fieldSet.readInt("year"));
                    g.setTeam(fieldSet.readString("team"));
                    g.setWeek(fieldSet.readInt("week"));
                    g.setOpponent(fieldSet.readString("opponent"));
                    g.setCompletes(fieldSet.readInt("completes"));
                    g.setAttempts(fieldSet.readInt("attempts"));
                    g.setPassingYards(fieldSet.readInt("passingYards"));
                    g.setPassingTd(fieldSet.readInt("passingTd"));
                    g.setInterceptions(fieldSet.readInt("interceptions"));
                    g.setRushes(fieldSet.readInt("rushes"));
                    g.setRushYards(fieldSet.readInt("rushYards"));
                    g.setReceptions(fieldSet.readInt("receptions", 0));
                    g.setReceptionYards(fieldSet.readInt("receptionYards"));
                    g.setTotalTd(fieldSet.readInt("totalTd"));
                    return g;
                })
                .build();
    }

    @Bean
    public Step gameLoadStep(FlatFileItemReader<Game> gameItemReader,
                             JdbcBatchItemWriter<Game> gameWriter) {
        return stepBuilderFactory.get("gameLoad")
                .<Game, Game>chunk(commitInterval)
                .reader(gameItemReader)
                .writer(gameWriter)
                .build();
    }

    @Bean
    public Step playerLoadStep(FlatFileItemReader<Player> playerItemReader,
                               JdbcBatchItemWriter<Player> playerDbWriter
    ) {
        return stepBuilderFactory.get("playerLoadStep")
                .<Player, Player>chunk(commitInterval)
                .reader(playerItemReader)
//                .processor(processor)
                .writer(playerDbWriter)
                .build();
    }

    @Bean
    public Step summarizationStep(
            JdbcCursorItemReader<PlayerSummary> playerSummaryJdbcReader,
            JdbcPlayerSummaryDao summaryWriter
    ) {
        return stepBuilderFactory.get("summarizationStep")
                .<PlayerSummary, PlayerSummary>chunk(commitInterval)
                .reader(playerSummaryJdbcReader)
                .writer(summaryWriter)
                .build();
    }

    @Bean
    public ItemWriter<Player> playerLogWriter() {
        return new ListItemWriter<>();
    }

    @Bean
    public JdbcBatchItemWriter<Player> playerDbWriter() {
        return new JdbcBatchItemWriterBuilder<Player>()
                .dataSource(dataSource)
                .sql("INSERT INTO player (player_id, last_name, first_name, position, birth_year, debut_year)" +
                        " VALUES (:id, :lastName, :firstName, :position, :birthYear, :debutYear)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Game> gameDbWriter() {
        return new JdbcBatchItemWriterBuilder<Game>()
                .dataSource(dataSource)
                .sql("INSERT INTO game (player_id, year, team, week, opponent, completes," +
                        "attempts, passing_yards, passing_td, interceptions, rushes," +
                        "rush_yards, receptions, reception_yards, total_td)" +
                        " VALUES (:id, :year, :team, :week, :opponent, :completes, :attempts," +
                        ":passingYards, :passingTd, :interceptions, :rushes, :rushYards, " +
                        ":receptions, :receptionYards, :totalTd)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcPlayerSummaryDao summaryWriter() {
        return new JdbcPlayerSummaryDao(dataSource);
    }
}
