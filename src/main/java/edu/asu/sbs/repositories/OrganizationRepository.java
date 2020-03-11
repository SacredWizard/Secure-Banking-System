package edu.asu.sbs.repositories;

import edu.asu.sbs.models.Organization;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.repository.CrudRepository;

public interface OrganizationRepository extends CrudRepository<Organization,Long> {
}
