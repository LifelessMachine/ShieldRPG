package com.arthas.shieldrpg.api;

public interface ShieldTriggerListener extends ShieldProvider {

    void onTrigger(ShieldSkillContext context);
}
