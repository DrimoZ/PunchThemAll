package com.drimoz.punchthemall.core.registry;

/**
 * The handful of constants still used by the schema_version 2 pipeline. The large set of v1 JSON key
 * constants was dropped with the legacy parser during the NeoForge port.
 */
public class RegistryConstants {

    public static final String SAME_STATE = "copy_state_value";

    public static final String INCORRECT_FORMAT = "Incorrect Json format";

    public static final String STRING_SCHEMA_VERSION = "schema_version";
}
