package com.wzdctool.android

object Constants {
    // location Service specific
    const val LOCATION_SERVICE_ID: Int = 175
    const val ACTION_START_LOCATION_SERVICE: String = "startLocationService"
    const val ACTION_STOP_LOCATION_SERVICE: String = "stopLocationService"

    // Names of linked azure blob storage containers
    const val AZURE_PUBLISHED_CONFIG_FILES_CONTAINER: String = "publishedconfigfiles"
    const val AZURE_PATH_DATA_UPLOADS_CONTAINER: String = "workzoneuploads"

    // Hashes of azure connection string and keys
    // Could not figure out how to effectively test connection string, so just verify that entered
    // connection string keys match the known has code
    const val AZURE_CONNECTION_STRING_HASH_CODE: Int = 200236452
    const val AZURE_ACCOUNT_NAME_HASH_CODE: Int = 71349687
    const val AZURE_ACCOUNT_KEY_HASH_CODE: Int = -167324863

    // Application-wide file locations, populated by main activity on start
    var CONFIG_DIRECTORY: String = ""
    var DATA_FILE_DIRECTORY: String = ""
    var PENDING_UPLOAD_DIRECTORY: String = ""
    var DOWNLOAD_LOCATION: String = ""
    var RECENT_WZ_MAPS: String = ""
}