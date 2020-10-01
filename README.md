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

- [V2X Manual Data Collection](https://github.com/TonyEnglish/V2X-manual-data-collection)
- [V2X Azure Functions](https://github.com/TonyEnglish/V2X_AzureFunctions)

## Prerequisites

*Required - Detail what actions users need to take before they can stand up the project, including instructions for different environments users might have. This might include instructions and examples for installing additional software.*

Example:

Requires:
- Java 8 (or higher)
- Maven 3.5.4
- Docker

## Usage
*Required - Provide users with detailed instrucitons for how to use your software. The specifics of this section will vary between projects, but should adhere to the following minimum outline:*

### Building

No building/compiling is required for this tool.

### Testing

There are currently no test cases for this proof of concept tool.

### Execution

#### Step 1
![Import Configuration File UI](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Import_Configuration_File.jpg)
The first step in mapping a work zone is importing the configuration file already created for the work zone. This file contains basic information, such as the number of lanes and the speed limits in the work zone. A configuration file can be created at https://neaeraconsulting.com/V2X_ConfigCreator. Once a configuration file is selected, the next button brings the user to the data collection component

#### Step 2
![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Data_Collection.jpg)
The next step is to physically map the work zone. This involves driving through the work zone while marking features in the mobile application. These features include lane closures/openings and the presence of workers. In automatic detection mode, the application will begin and end data collection based on the locations set in the configuration file. In manual detection mode, the user must start and end data collection, and the application will save these locations to allow the application to run automatically the next time this work zone is mapped.

#### Step 3
![Collect Path Data](https://github.com/TonyEnglish/V2X_MobileApplication/blob/dev/Images/Path_Data_Uploaded.jpg)
The final step is to upload the generated path data file. The application will execute this step automatically at the end of data collection, and a notification (shown above) will be shown. If the path data file fails to upload, you can upload it manually to https://neaeraconsulting.com/V2X_Upload (Generated data files can be found at Android/data/com.wzdctool.android/files/Download)

## Additional Notes

This toolset was developed as a proof of concept and is not able to provide a full solution for all types of work zones. Future work may expand the functionality of the tool to address more work zone types and add other features such as authentication or a mobile app version of the tool.

This tool functions alongside a POC TMC website (https://neaeraconsulting.com/V2x_Home). Instructions for utilizing this website are located here: [POC Toolset User Guide](https://github.com/TonyEnglish/V2X-manual-data-collection/blob/master/POC%20Toolset%20User%20Guide.pdf)

Documentation for this project is located here: [Documentation](https://github.com/TonyEnglish/V2X-manual-data-collection/tree/master/Documentation). This documentation includes:

- V2X POC Interface Control Document
- V2X POC System Engineering Report
- V2X POC Test Case Results Report

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
