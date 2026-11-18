import datetime as dt
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('refresh', Path(__file__).parents[1] / 'refresh-ipad.py')
refresh = importlib.util.module_from_spec(spec)
spec.loader.exec_module(refresh)


class RenewalSafetyTest(unittest.TestCase):
    def setUp(self):
        self.time = dt.datetime(2026, 11, 16, 12, tzinfo=dt.timezone.utc)
        self.state = dict(expiration=(self.time + dt.timedelta(hours=48)).isoformat(), sourceFingerprint='approved')

    def run_renewal(self, **changes):
        patches = dict(now=lambda: self.time, fingerprint=lambda: 'approved', emit=lambda *args, **kwargs: None)
        patches.update(changes)
        with patch.multiple(refresh, **patches), patch.object(refresh, 'command') as command:
            refresh.renew(self.state, False)
            command.assert_not_called()

    def test_healthy_profile_does_not_contact_device(self):
        self.state['expiration'] = (self.time + dt.timedelta(days=6)).isoformat()
        with patch.object(refresh, 'device_json', side_effect=AssertionError('Should not query iPad')):
            self.run_renewal()

    def test_unreviewed_source_does_not_contact_device_or_build(self):
        with patch.object(refresh, 'device_json', side_effect=AssertionError('Should not query iPad')):
            self.run_renewal(fingerprint=lambda: 'modified')

    def test_running_app_is_not_interrupted(self):
        details = dict(connectionProperties=dict(pairingState='paired'), deviceProperties=dict(developerModeStatus='enabled'))
        with patch.object(refresh, 'device_json', return_value=details), patch.object(refresh, 'in_use', return_value=True):
            self.run_renewal()

    def test_unpaired_device_is_not_modified(self):
        details = dict(connectionProperties=dict(pairingState='unpaired'), deviceProperties=dict(developerModeStatus='enabled'))
        with patch.object(refresh, 'device_json', return_value=details):
            self.run_renewal()

    def test_unreachable_device_is_deferred(self):
        with patch.object(refresh, 'device_json', side_effect=refresh.subprocess.TimeoutExpired('devicectl', 20)):
            self.run_renewal()


if __name__ == '__main__':
    unittest.main()
