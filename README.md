# V2X_MobileApplication
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=TonyEnglish_V2X_MobileApplication&metric=bugs)](https://sonarcloud.io/dashboard?id=TonyEnglish_V2X_MobileApplication)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=TonyEnglish_V2X_MobileApplication&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=TonyEnglish_V2X_MobileApplication)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=TonyEnglish_V2X_MobileApplication&metric=alert_status)](https://sonarcloud.io/dashboard?id=TonyEnglish_V2X_MobileApplication)
[![Build Status](https://travis-ci.org/TonyEnglish/V2X_MobileApplication.svg?branch=master)](https://travis-ci.org/TonyEnglish/V2X_MobileApplication)

## Project Description

This project is an open source, proof of concept work zone data collection tool. The purpose of this tool is to allow a construction manager in the field and transportation system manager at the Infrastructure Owner Operator (IOO) back-office  to map work zones and distribute generated map messages to third parties. This project is part of a larger effort on understanding mapping needs for V2X applications, funded by USDOT. This repository is a deliverable under the project and supports the Development and Demonstration of Proof-of-Concept of an Integrated Work Zone Mapping Toolset.

This repository contains the following components:

- POC Work Zone Data Collection (WZDC) tool
  - Mobile Application (Android)
  
### Related Projects

- [Work Zone Data Collection Tool](https://github.com/TonyEnglish/V2X-manual-data-collection)
- [V2X Azure Functions](https://github.com/TonyEnglish/V2X_AzureFunctions)

## Prerequisites
Supported device running Android 8.0 to 11 (SDK 26 - 30)

Internet access (Loading configuration files and uploading path data)

## Usage
This application is located on the Google play store: [WZDC Tool](https://play.google.com/store/apps/details?id=com.wzdctool.android)

### Building

No building/compiling is required for this tool.

### Testing

There are currently no test cases for this proof of concept tool.

### Execution

Main Page | Configuration | Data Collection
:-------------------------:|:-------------------------:|:-------------------------:
![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Main_Page.jpg)  | ![Import Configuration File UI](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Import_Configuration_File.jpg)  |  ![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Data_Collection.jpg)

#### Setup: Configure Azure Connection Info
Contact [tony@neaeraconsulting.com](mailto://tony@neaeraconsulting.com) to request connection to the cloud. 

#### Step 1: Configuration
At the top of the screen, there are 3 elements. The highest is a textbox labeled Active Config. This displays the work zone id of the active configuration file. The next element is a slider showing Internal GPS and USb GPS. Each GPS type ahs the following stats

Internal GPS:
- Green: GPS connection is valid and can be used for data collection
- Yellow: Location permissions are enabled but location is currently disabled. GPS type cannot currently be used for data collection
- Red: Location permissions are disabled. GPS type cannot currently be used for data collection

USB (External) GPS:
- Green: GPS connection is valid and can be used for data collection
- Yellow: Device detected but has not established a valid GPS fix. . GPS type cannot currently be used for data collection
- Red: No supported USB devices are detected. GPS type cannot currently be used for data collection

The final element at the top of the screen features 2 checkboxes. These display whether those message types will be generated in the current configuration. RSM messages may only be generated when the GPS accuracy is less than 2 meters. 

The Manual Detection vs Automatic Detection slider changes the start/ending location detection mode. 
1. In automatic detection mode (default), data collection will automatically commence when a set starting location is reached (from configuration file). Data collection will commence as normal, until the set ending location is reached, at which point data collection will end and the data file will be uploaded. 
2. In manual detection mode, the user manually starts and ends data collection. When the user is approaching a work zone, the user presses the play button. When the work zone begins, the user presses the marker button. Then, data collection commences as normal. Once the user exits the work zone, they will press the stop button.

The final step is to select a configuration file. This file contains basic information, such as the number of lanes and the speed limits in the work zone. A configuration file can be created at https://neaeraconsulting.com/V2X_ConfigCreator.

Note: The user may only start data collection if a valid GPS device is selected and a configuration file is selected. 

#### Step 2: Data Collection
Data collection functions slightly differently in manual and automatic detection modes. The application behaviorn is described below
1. In automatic detection mode (default), data collection will automatically commence when a set starting location is reached (from configuration file). Data collection will commence as normal, until the set ending location is reached, at which point data collection will end and the data file will be uploaded. 
2. In manual detection mode, the user manually starts and ends data collection. When the user is approaching a work zone, the user presses the play button. When the work zone begins, the user presses the marker button. Then, data collection commences as normal. Once the user exits the work zone, they will press the stop button.

The user is (usually) required to drive in a specific lane, except in cases where the user does not intend to generate RSM messages. This lane is set in the configuration file and shown in the data collection screen by a car icon.

Once data collection has begun, the user can mark lane closures and the presence of workers. 
Lane closures are marked at the beginning of the taper. For a closing lane, mark the lane closed when the lane starts to taper to closed. For an opening lane, mark the lane open when the lane starts to taper to open. To mark a lane closure in the application, simply click the corresponding lane button. A cone will appear, signifying that the lane is closed. To open that lane, simply press the lane again. The lane with a yellow/orange car displays the lane that the user is driving in and cannot be closed. Images are shown below
To mark workers present, press the worker button at the bottom. The background will change color and the worker will be colored in, signifying that workers are present. To mark workers no longer present, simply press the worker button again. Images are shown below

Nothing  |  Lane Closed  |  Workers Present
:-------------------------:|:-------------------------:|:-------------------------:
![Import Configuration File UI](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Data_Collection_Blank_cropped.jpg)  |  ![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Lane_Closed_cropped.jpg)  |  ![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Workers_Present_cropped.jpg)

For more detailed user information, see the [WZDC Tool Mobile Application Setup + Documentation](https://github.com/TonyEnglish/V2X_MobileApplication/tree/master/Documentation/WZDC%Tool%Mobile%Application%Setup%+%Documentation.pdf)

#### Step 3: Upload Path Data
The final step is to upload the generated path data file. The application will execute this step automatically at the end of data collection, and a notification (shown above) will be shown. If the path data file fails to upload, you can upload it manually to https://neaeraconsulting.com/V2X_Upload (Generated data files can be found at Android/data/com.wzdctool.android/files/Download)

## Additional Notes

This toolset was developed as a proof of concept and is not able to provide a full solution for all types of work zones. Future work may expand the functionality of the tool to address more work zone types and add other features such as authentication or a mobile app version of the tool.

This tool functions alongside a POC TMC website (https://neaeraconsulting.com/V2x_Home). Instructions for utilizing this website are located here: [POC Toolset User Guide](https://github.com/TonyEnglish/V2X-manual-data-collection/blob/master/POC%20Toolset%20User%20Guide.pdf)

Documentation for this project is located here: [Documentation](https://github.com/TonyEnglish/V2X_MobileApplication/tree/master/Documentation). This documentation includes:

- V2X POC Interface Control Document
- V2X POC System Engineering Report
- V2X POC Test Case Results Report
- WZDC Tool Documentation Updates
  - Describes message generation process
- WZDC Tool Mobile Application Setup + Documentation
  - Describes how to use application in detail
  - Describes how to set up development environment

## Version History and Retention

**Status:** prototype

**Release Frequency:** This project is updated approximately once every 2 weeks

**Release History:** See [CHANGELOG.md](https://github.com/TonyEnglish/V2X_MobileApplication/blob/master/CHANGELOG.md)

**Retention:** This project will remain publicly accessible for a minimum of five years (until at least 08/15/2025).

## License

This project is licensed under the MIT License - see the [License.md](https://github.com/TonyEnglish/V2X_MobileApplication/blob/master/LICENSE.md) for more details. 

## Contributions

Instructions are listed below, please read [CONTRIBUTING.md](https://github.com/TonyEnglish/V2X_MobileApplication/blob/master/CONTRIBUTING.md) for more details.

- Report bugs and request features via [Github Issues](https://github.com/TonyEnglish/V2X_MobileApplication/issues).
- [Email us](mailto://tony@neaeraconsulting.com) your ideas on how to improve this proof of concept toolset.
- Create a [Github pull request](https://github.com/TonyEnglish/V2X_MobileApplication/pulls) with new functionality or fixing a bug.
- Triage tickets and review update-tickets created by other users.

### Guidelines

- Issues
  - Create issues using the SMART goals outline (Specific, Measurable, Actionable, Realistic and Time-Aware)
- PR (Pull Requests)
  - Create all pull requests from the master branch
  - Create small, narrowly focused PRs
  - Maintain a clean commit history so that they are easier to review


## Contact Information

Contact Name: Tony English
Contact Information: [tony@neaeraconsulting.com](mailto://tony@neaeraconsulting.com)

## Acknowledgements
