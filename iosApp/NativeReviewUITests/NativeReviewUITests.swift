import XCTest

final class NativeReviewUITests: XCTestCase {
    func testLandscapeDayAndNight() throws {
        continueAfterFailure = false
        let application = XCUIApplication(bundleIdentifier: "app.geeflow.debug")
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        for scene in ["dashboard", "temperature"] {
            for theme in ["day", "night"] {
                XCUIDevice.shared.orientation = .landscapeLeft
                application.launchArguments = ["--design-review", scene, "--" + theme,
                    "-AppleLanguages", "(ko)", "-AppleLocale", "ko_KR"]
                application.launch()
                XCTAssertTrue(application.wait(for: .runningForeground, timeout: 30))
                XCUIDevice.shared.orientation = .landscapeLeft
                let landscape = NSPredicate { _, _ in application.frame.width > application.frame.height }
                expectation(for: landscape, evaluatedWith: application)
                waitForExpectations(timeout: 30)
                // Allow the native material and Compose text to settle after rotation.
                Thread.sleep(forTimeInterval: 4)
                let screenshot = XCUIScreen.main.screenshot()
                XCTAssertGreaterThan(screenshot.image.size.width, screenshot.image.size.height)
                let name = "ipad-\(scene)-\(theme)"
                try screenshot.pngRepresentation.write(to: directory.appendingPathComponent(name + ".png"))
                let attachment = XCTAttachment(screenshot: screenshot)
                attachment.name = name
                attachment.lifetime = .keepAlways
                add(attachment)
                application.terminate()
            }
        }
    }
}
