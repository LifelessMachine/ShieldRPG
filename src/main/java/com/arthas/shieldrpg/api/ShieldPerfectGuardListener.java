package com.arthas.shieldrpg.api;

public interface ShieldPerfectGuardListener extends ShieldProvider {

    void onPerfectGuard(PerfectGuardContext context);
}
