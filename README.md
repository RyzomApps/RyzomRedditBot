# RyzomRedditBot

[![reddit](https://img.shields.io/reddit/subreddit-subscribers/Ryzom)](https://old.reddit.com/r/Ryzom/)
![repo size](https://img.shields.io/github/languages/code-size/RyzomApps/RCC.svg?label=repo%20size)
![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)
![Java](https://img.shields.io/badge/Language-Java-orange)

## Description
A Java-based Reddit bot that scrapes release notes from the Ryzom website and posts new updates automatically to a specified subreddit. It tracks posted news to avoid duplicates and formats posts with Markdown, including images and key points.

## Features
- Fetches and parses release notes from [Ryzom's release page](https://app.ryzom.com/app_releasenotes/index.php?lang=en&ig=1)
- Posts formatted Markdown updates to Reddit with flair
- Keeps track of posted news to prevent duplicates
- Supports images as links and structured headlines with key points
- Configurable via `config.properties`

## Usage
1. Clone the repository
2. Set up your `config.properties` with your Reddit credentials and target subreddit
3. Run the Java application

## Warranty
Please note: all tools/scripts in this repo are released for use "AS IS" without any warranties of any kind, including, but not limited to their installation, use, or performance. We disclaim any and all warranties, either express or implied, including but not limited to any warranty of noninfringement, merchantability, and/or fitness for a particular purpose. We do not warrant that the technology will meet your requirements, that the operation thereof will be uninterrupted or error-free, or that any errors will be corrected.

Any use of these scripts and tools is at your own risk. There is no guarantee that they have been through thorough testing in a comparable environment and we are not responsible for any damage or data loss incurred with their use.

You are responsible for reviewing and testing any scripts you run thoroughly before use in any non-testing environment.
