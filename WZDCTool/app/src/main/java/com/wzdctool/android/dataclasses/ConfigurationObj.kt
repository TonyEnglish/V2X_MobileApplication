package com.wzdctool.android.dataclasses

data class ConfigurationObj(
    val DateCreated: String,
    val FeedInfoID: String,
    val GeneralInfo: GENERALINFO,
    val TypesOfWork: List<TYPEOFWORK>,
    val LaneInfo: LANEINFO,
    val SpeedLimits: SPEEDLIMITS,
    val CauseCodes: CAUSE,
    val Schedule: SCHEDULE,
    val Location: LOCATION,
    val metadata: METADATA,
    val ImageInfo: IMAGEINFO
)

data class ConfigurationObjNoImage(
    val DateCreated: String,
    val FeedInfoID: String,
    val GeneralInfo: GENERALINFO,
    val TypesOfWork: List<TYPEOFWORK>,
    val LaneInfo: LANEINFO,
    val SpeedLimits: SPEEDLIMITS,
    val CauseCodes: CAUSE,
    val Schedule: SCHEDULE,
    val Location: LOCATION,
    val metadata: METADATA
)

data class GENERALINFO(
    val Description: String,
    val RoadName: String,
    val RoadNumber: String,
    val Direction: DIRECTION?,
    val BeginningCrossStreet: String, //beginning cross street
    val EndingCrossStreet: String, //ending cross street
    val BeginningMilePost: Int, //int beginning milepost
    val EndingMilePost: Int, //int ending milepost
    val EventStatus: EVENTSTATUS? //calculated
)

data class IMAGEINFO(
    val Zoom: Int,
    val Center: Coordinate,
    val Markers: List<Marker>,
    val MapType: String,
    val Height: Int,
    val Width: Int,
    val Format: String,
    val ImageString: String
)
data class Marker(
    val Name: String,
    val Color: String,
    val Location: Coordinate
)

data class LANEINFO(
    val NumberOfLanes: Int,
    val AverageLaneWidth: Double,
    val ApproachLanePadding: Double,
    val WorkzoneLanePadding: Double,
    val VehiclePathDataLane: Int,
    val Lanes: List<LANE>
)
data class METADATA(
    val wz_location_method: WZ_LOCATION_METHODS?,
    val lrs_type: String,
    val location: String,
    val datafeed_frequency_update: String?,
    val timestamp_metadata_update: String,
    val contact_name: String,
    val contact_email: String,
    val issuing_organization: String
)
data class LANE(
    val LaneNumber: Int,
    val LaneType: LANETYPES,
    val LaneRestrictions: List<LANERESTRICTIONS>
)

data class LANERESTRICTIONS(
    val RestrictionValue: Float?,
    val RestrictionType: RESTRICTIONTYPE,
    val RestrictionUnits: RESTRICTIONUNITS?
)

data class TYPEOFWORK(
    val WorkType: WORKTYPE?,
    val Is_Architectural_Change: Boolean
)

data class SPEEDLIMITS(
    val NormalSpeed: Int,
    val ReferencePointSpeed: Int,
    val WorkersPresentSpeed: Int
)

data class CAUSE(
    val CauseCode: Int,
    val SubCauseCode: Int
)

data class SCHEDULE(
    val StartDate: String,
    val StartDateAccuracy: ACCURACY?,
    val EndDate: String,
    val EndDateAccuracy: ACCURACY?,
    val DaysOfWeek: List<String>
)

data class LOCATION(
    val BeginningLocation: Coordinate,
    val BeginningAccuracy: ACCURACY?,
    val EndingLocation: Coordinate,
    val EndingAccuracy: ACCURACY?
)

data class Coordinate(
    val Lat: Double,
    val Lon: Double,
    val Elev: Double?
)

enum class EVENTSTATUS
{
    planned,
    pending,
    active,
    cancelled,
    completed
}
enum class ACCURACY
{
    estimated,
    verified
}
enum class DIRECTION
{
    northbound,
    eastbound ,
    southbound,
    westbound
}
enum class WORKTYPE
{
    maintenance,
    `minor-road-defect-repair`,
    `roadside-work`,
    `overhead-work`,
    `below-road-work`,
    `barrier-work`,
    `surface-work`,
    painting,
    `roadway-relocation`,
    `roadway-creation`
}
enum class RESTRICTIONTYPE
{
    `no-trucks`,
    `travel-peak-hours-only`,
    `hov-3`,
    `hov-2`,
    `no-parking`,
    `reduced-width`,
    `reduced-height`,
    `reduced-length`,
    `reduced-weight`,
    `axle-load-limit`,
    `gross-weight-limit`,
    `towing-prohibited`,
    `permitted-oversize-loads-prohibitied`
}
enum class LANETYPES
{
    `all-roadways`,
    `through-lanes`,
    `left-lane`,
    `right-lane`,
    `center-lane`,
    `middle-lane`,
    `middle-two-lanes`,
    `right-turning-lanes`,
    `left-turing-lanes`,
    `right-exit-lanes`,
    `left-exit-lanes`,
    `right-mergining-lanes`,
    `left-merging-lanes`,
    `right-exit-ramp`,
    sidewalk,
    `bike-lane`,
    `right-shoulder-outside`,
    `left-shoulder`
}
enum class RESTRICTIONUNITS
{
    feet,
    inches,
    centimeters,
    pounds,
    tons,
    kilograms
}
enum class WZ_LOCATION_METHODS
{
    `channel-device-method`,
    `sign-method`,
    `junction-method`,
    unknown,
    other
}