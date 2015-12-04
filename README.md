## Synopsis

Components for monitoring NICS services

## Dependencies
- nics-assembly
- nics-common

## Building

	mvn package


## Description

- host-ping-alert - Service that will PING a list of hostnames/IPs, as well as webpages with specific text, and send email alerts (using email-consumer) if the host doesn't respond, or site if the webpage does not load and contain the specified text

## Documentation

Further documentation is available at nics-common/docs
