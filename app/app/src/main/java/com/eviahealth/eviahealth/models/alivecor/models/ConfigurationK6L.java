package com.eviahealth.eviahealth.models.alivecor.models;

import com.alivecor.api.FilterType;
import com.alivecor.api.LeadConfiguration;
import com.alivecor.api.RecordingConfiguration;

import org.json.JSONObject;

public class ConfigurationK6L {

    private String extras;
    private Boolean enableLeadsButtons = false;
    private LeadConfiguration leadConfiguration = LeadConfiguration.SINGLE;
    private FilterType filterType = FilterType.ENHANCED;
    private Integer maxDuration = 30;
    private Integer resetDuration = 10;
    private Integer mainsFrequency = RecordingConfiguration.MAINS_FREQUENCY_50Hz;

    public ConfigurationK6L(String configDevie) {
        try {
            if ((configDevie != null) && (!configDevie.isEmpty())){
                JSONObject obj = new JSONObject(configDevie);
                if (obj.has("enableLeadsButtons")) {
                    enableLeadsButtons = obj.getBoolean("enableLeadsButtons");
                }
                if (obj.has("leadConfiguration")) {
                    String lead = obj.getString("leadConfiguration");
                    leadConfiguration = (lead.equals("SIX")) ? LeadConfiguration.SIX : LeadConfiguration.SINGLE;
                }
                if (obj.has("filterType")) {
                    String leadB = obj.getString("filterType");
                    filterType = (leadB.equals("ENHANCED")) ? FilterType.ENHANCED : FilterType.ORIGINAL;
                }
                if (obj.has("maxDuration")) {
                    maxDuration = obj.getInt("maxDuration");
                }
                if (obj.has("resetDuration")) {
                    resetDuration = obj.getInt("resetDuration");
                }
                if (obj.has("mainsFrequency")) {
                    mainsFrequency = obj.getInt("mainsFrequency");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean getEnableLeadsButtons() {
        return enableLeadsButtons;
    }

    public void setEnableLeadsButtons(Boolean enableLeadsButtons) {
        this.enableLeadsButtons = enableLeadsButtons;
    }

    public LeadConfiguration getLeadConfiguration() {
        return leadConfiguration;
    }

    public void setLeadConfiguration(LeadConfiguration leadConfiguration) {
        this.leadConfiguration = leadConfiguration;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public Integer getResetDuration() {
        return resetDuration;
    }

    public void setResetDuration(Integer resetDuration) {
        this.resetDuration = resetDuration;
    }

    public Integer getMainsFrequency() {
        return mainsFrequency;
    }

    public void setMainsFrequency(Integer mainsFrequency) {
        this.mainsFrequency = mainsFrequency;
    }

    @Override
    public String toString() {
        try {
            JSONObject params = new JSONObject();
            params.put("enableLeadsButtons", enableLeadsButtons);
            params.put("leadConfiguration", leadConfiguration.toString());
            params.put("filterType", filterType.toString());
            params.put("maxDuration", maxDuration);
            params.put("resetDuration", resetDuration);
            params.put("mainsFrequency", mainsFrequency);
            return params.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
