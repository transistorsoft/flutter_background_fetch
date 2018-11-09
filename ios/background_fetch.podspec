#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'background_fetch'
  s.version          = '0.0.1'
  s.summary          = 'Periodic callbacks in the background for both IOS and Android.'
  s.description      = <<-DESC
Periodic callbacks in the background for both IOS and Android.
                       DESC
  s.homepage         = 'https://github.com/transistorsoft/flutter_background_fetch'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Transistor Software' => 'info@transistorsoft.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.vendored_frameworks = ['TSBackgroundFetch.framework']
  s.ios.deployment_target = '8.0'
end

