package com.pet.auditspringbootstarter.audit;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<String> getCurrentUsername();
}
