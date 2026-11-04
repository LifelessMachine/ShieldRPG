package com.arthas.shieldrpg.api;

public interface ShieldSkillAction extends ShieldProvider {

    void execute(ShieldSkillContext context, ShieldSkillParams params);
}
