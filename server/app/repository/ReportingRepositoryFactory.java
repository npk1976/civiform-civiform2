package repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import models.ProgramModel;
import services.program.ProgramDefinition;

/**
 * Factory for initializing ReportingRepository.
 *
 * <p>Avoids n+1 DB queries when acquiring the public name of a program when loading program
 * reporting statistics.
 */
public final class ReportingRepositoryFactory {
  private final Clock clock;
  private final Database database;
  private final VersionRepository versionRepository;

  @Inject
  public ReportingRepositoryFactory(Clock clock, VersionRepository versionRepository) {
    this.clock = Preconditions.checkNotNull(clock);
    this.database = DB.getDefault();
    this.versionRepository = Preconditions.checkNotNull(versionRepository);
  }

  /**
   * Creating a ReportingRepository object with <code>List&lt;ProgramModel&gt;</code> output as a
   * HashMap.
   */
  public ReportingRepository create() {
    ImmutableList<ProgramModel> listOfPrograms = versionRepository.getActiveVersion().getPrograms();
    ImmutableMap.Builder<String, String> programMapBuilder = new ImmutableMap.Builder<>();
    for (ProgramModel p : listOfPrograms) {
      ProgramDefinition pd = p.getProgramDefinition();
      programMapBuilder.put(pd.adminName(), pd.localizedName().getDefault());
    }
    ImmutableMap<String, String> programToPublicHash = programMapBuilder.build();
    return new ReportingRepository(clock, database, programToPublicHash);
  }
}
