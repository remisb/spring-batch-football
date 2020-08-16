package lt.bauzys.sbatch.footbal;

import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

public class JdbcPlayerSummaryDao implements ItemWriter<PlayerSummary> {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final String INSERT_SUMMARY = "INSERT into player_summary(ID, YEAR_NO, COMPLETES, ATTEMPTS, PASSING_YARDS, PASSING_TD, "
            + "INTERCEPTIONS, RUSHES, RUSH_YARDS, RECEPTIONS, RECEPTION_YARDS, TOTAL_TD) "
            + "values(:id, :year, :completes, :attempts, :passingYards, :passingTd, "
            + ":interceptions, :rushes, :rushYards, :receptions, :receptionYards, :totalTd)";

    public JdbcPlayerSummaryDao(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void write(List<? extends PlayerSummary> summaries) {

        for (PlayerSummary summary : summaries) {
            MapSqlParameterSource args = new MapSqlParameterSource()
                    .addValue("id", summary.getId())
                    .addValue("year", summary.getYear())
                    .addValue("completes", summary.getCompletes())
                    .addValue("attempts", summary.getAttempts())
                    .addValue("passingYards", summary.getPassingYards())
                    .addValue("passingTd", summary.getPassingTd())
                    .addValue("interceptions", summary.getInterceptions())
                    .addValue("rushes", summary.getRushes())
                    .addValue("rushYards", summary.getRushYards())
                    .addValue("receptions", summary.getReceptions())
                    .addValue("receptionYards", summary.getReceptionYards())
                    .addValue("totalTd", summary.getTotalTd());

            namedParameterJdbcTemplate.update(INSERT_SUMMARY, args);
        }
    }
}
