var app = angular.module('sentinelDashboardApp');

app.service('IdentityServiceV2', ['$http', function ($http) {

  this.fetchIdentityOfMachine = function (searchKey,app) {
    var param = {
      searchKey: searchKey,
      app: app
    };
    return $http({
      url: '/v2/resource/machineResource.json',
      params: param,
      method: 'GET'
    });
  };
  this.fetchClusterNodeOfMachine = function (searchKey,app) {
    var param = {
      type: 'cluster',
      searchKey: searchKey,
      app: app
    };
    return $http({
      url: '/v2/resource/machineResource.json',
      params: param,
      method: 'GET'
    });
  };
}]);
